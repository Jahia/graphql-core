import {request as httpRequest} from 'http';
import {request as httpsRequest} from 'https';
import {randomBytes} from 'crypto';
import {Socket} from 'net';

interface HandshakeOptions {
    url: string
    origin?: string
    cookie?: string
    protocol?: string
    timeout?: number
    headers?: Record<string, string>
}

interface HandshakeResult {
    status: number
    messages: string[]
    closeCode: number | null
    error?: string
}

const NORMAL_CLOSURE = 1000;

const maskedFrame = (opcode: number, payload: Buffer): Buffer => {
    const mask = randomBytes(4);
    const header = payload.length < 126 ?
        Buffer.from([0x80 | opcode, 0x80 | payload.length]) :
        Buffer.from([0x80 | opcode, 0x80 | 126, payload.length >> 8, payload.length & 0xff]);
    const masked = Buffer.from(payload.map((b, i) => b ^ mask[i % 4]));
    return Buffer.concat([header, mask, masked]);
};

const textFrame = (text: string): Buffer => maskedFrame(0x1, Buffer.from(text));

const closeFrame = (code: number): Buffer => maskedFrame(0x8, Buffer.from([code >> 8, code & 0xff]));

// Reads the unmasked server frames held in buffer, returns the bytes left over.
const readFrames = (buffer: Buffer, onFrame: (opcode: number, data: Buffer) => void): Buffer => {
    let rest = buffer;
    while (rest.length >= 2) {
        let length = rest[1] & 0x7f;
        let offset = 2;
        if (length === 126) {
            if (rest.length < 4) {
                break;
            }

            length = rest.readUInt16BE(2);
            offset = 4;
        } else if (length === 127) {
            if (rest.length < 10) {
                break;
            }

            length = Number(rest.readBigUInt64BE(2));
            offset = 10;
        }

        if (rest.length < offset + length) {
            break;
        }

        onFrame(rest[0] & 0x0f, rest.subarray(offset, offset + length));
        rest = rest.subarray(offset + length);
    }

    return rest;
};

const messageType = (data: Buffer): string => {
    try {
        return JSON.parse(data.toString()).type;
    } catch (e) {
        return `unparsed: ${data.toString()}`;
    }
};

// Opens a WebSocket to url with the given Origin and Cookie headers, which page scripts cannot set, and sends
// connection_init. Resolves with the handshake status and the messages received, once connection_ack arrives (the
// connection is then closed normally), once the server closes the connection (with its close code), or at the timeout.
const wsHandshake = (options: HandshakeOptions): Promise<HandshakeResult> => {
    return new Promise((resolve, reject) => {
        const target = new URL(options.url);
        const secure = target.protocol === 'wss:' || target.protocol === 'https:';
        const headers: Record<string, string> = {
            Connection: 'Upgrade',
            Upgrade: 'websocket',
            'Sec-WebSocket-Version': '13',
            'Sec-WebSocket-Key': randomBytes(16).toString('base64'),
            'Sec-WebSocket-Protocol': options.protocol || 'graphql-ws'
        };
        if (options.origin !== undefined) {
            headers.Origin = options.origin;
        }

        if (options.cookie) {
            headers.Cookie = options.cookie;
        }

        Object.assign(headers, options.headers);

        const result: HandshakeResult = {status: 0, messages: [], closeCode: null};
        const timeout = options.timeout || 5000;
        const request = secure ? httpsRequest : httpRequest;
        const req = request({
            hostname: target.hostname,
            port: target.port || (secure ? 443 : 80),
            path: target.pathname,
            headers
        });
        req.on('response', res => {
            result.status = res.statusCode;
            res.resume();
            resolve(result);
        });
        req.setTimeout(timeout, () => req.destroy(new Error(`no handshake response from ${options.url} within ${timeout} ms`)));
        req.on('upgrade', (res, socket: Socket, head: Buffer) => {
            req.setTimeout(0);
            result.status = res.statusCode;
            let settled = false;
            let pending = Buffer.alloc(0);
            const settle = () => {
                if (!settled) {
                    settled = true;
                    clearTimeout(timer);
                    socket.destroy();
                    resolve(result);
                }
            };

            const onFrame = (opcode: number, data: Buffer) => {
                if (settled) {
                    return;
                }

                if (opcode === 0x8) {
                    result.closeCode = data.length >= 2 ? data.readUInt16BE(0) : 1005;
                    settle();
                } else if (opcode === 0x1) {
                    const type = messageType(data);
                    result.messages.push(type);
                    if (type === 'connection_ack') {
                        settled = true;
                        clearTimeout(timer);
                        socket.end(closeFrame(NORMAL_CLOSURE));
                        resolve(result);
                    }
                }
            };

            const consume = (chunk: Buffer) => {
                pending = readFrames(Buffer.concat([pending, chunk]), onFrame);
            };

            const timer = setTimeout(settle, timeout);
            socket.on('data', consume);
            socket.on('close', settle);
            socket.on('error', (error: Error) => {
                if (!settled) {
                    result.error = error.message;
                }

                settle();
            });
            consume(head);
            if (!settled) {
                socket.write(textFrame(JSON.stringify({type: 'connection_init', payload: {}})));
            }
        });
        req.on('error', reject);
        req.end();
    });
};

module.exports = wsHandshake;

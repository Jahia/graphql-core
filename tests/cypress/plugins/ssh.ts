import {NodeSSH} from 'node-ssh';

interface connection {
    hostname: string
    port: string
    username: string
    password: string
}

const sshCommand = (commands: Array<string>, connection: connection) => {
    return new Promise((resolve, reject) => {
        const ssh = new NodeSSH();
        console.info('[SSH] connection to:', connection.hostname);
        console.info('[SSH] commands:', commands);
        console.info('[SSH] ', connection);
        ssh.connect({
            host: connection.hostname,
            port: connection.port,
            username: connection.username,
            password: connection.password
        })
            .then(() => {
                ssh.exec(commands.join(';'), [])
                    .then(function (result) {
                        resolve(result); // Resolve to command result
                    })
                    .catch(reason => {
                        console.error('[SSH] Failed to execute commands: ', commands);
                        reject(reason);
                    });
            })
            .catch(reason => {
                console.error('[SSH] Failed to execute commands: ', commands);
                reject(reason);
            });
    });
};

module.exports = sshCommand;

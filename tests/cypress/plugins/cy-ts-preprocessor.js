const wp = require('@cypress/webpack-preprocessor')
const webpack = require('webpack')

const webpackOptions = {
    resolve: {
        extensions: ['.ts', '.js'],
    },
    plugins: [
        new webpack.ProvidePlugin({
            Buffer: ['buffer', 'Buffer'],
        }),
    ],
    module: {
        rules: [
            {
                test: /\.ts$/,
                exclude: [/node_modules/],
                use: [
                    {
                        loader: 'ts-loader',
                    },
                ],
            },
            {
                test: /\.mjs$/,
                include: /node_modules/,
                type: 'javascript/auto',
            },
            {
                test: /\.groovy$/i,
                use: [
                    {
                        loader: 'raw-loader',
                        options: {
                            esModule: false,
                        },
                    },
                ],
                type: 'javascript/auto',
            },
        ],
    },
}

const options = {
    webpackOptions,
}

module.exports = wp(options)

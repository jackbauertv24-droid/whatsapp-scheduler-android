const path = require('path');
const webpack = require('webpack');

module.exports = {
    entry: './js-src/whatsapp-client.js',
    output: {
        filename: 'baileys-bundle.js',
        path: path.resolve(__dirname, 'app/src/main/assets/whatsapp'),
        library: 'WhatsApp',
        libraryTarget: 'window'
    },
    resolve: {
        fallback: {
            fs: false,
            path: false,
            os: false,
            crypto: false,
            stream: false,
            buffer: false,
            util: false,
            events: false
        }
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /node_modules/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        presets: ['@babel/preset-env'],
                        plugins: [
                            '@babel/plugin-transform-runtime'
                        ]
                    }
                }
            }
        ]
    },
    plugins: [
        new webpack.ProvidePlugin({
            Buffer: ['buffer', 'Buffer'],
            process: 'process/browser'
        })
    ],
    mode: 'production',
    target: 'web'
};
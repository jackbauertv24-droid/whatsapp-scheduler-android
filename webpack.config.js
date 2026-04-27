const path = require('path');

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
            buffer: require.resolve('buffer/'),
            util: require.resolve('util/'),
            events: require.resolve('events/')
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
        new (require('webpack')).ProvidePlugin({
            Buffer: ['buffer', 'Buffer'],
            process: 'process/browser'
        })
    ],
    mode: 'production',
    target: 'web'
};
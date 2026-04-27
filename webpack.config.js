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
            buffer: false,
            util: false,
            events: false,
            assert: false,
            url: false,
            zlib: false,
            readline: false,
            child_process: false,
            http: false,
            https: false,
            net: false,
            tty: false
        },
        alias: {
            'jimp': false,
            'sharp': false,
            'link-preview-js': false,
            'audio-decode': false
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
                            ['@babel/plugin-transform-runtime', { regenerator: true }]
                        ]
                    }
                }
            }
        ]
    },
    mode: 'production',
    target: 'web',
    externals: {
        'jimp': 'commonjs jimp',
        'sharp': 'commonjs sharp',
        'link-preview-js': 'commonjs link-preview-js',
        'audio-decode': 'commonjs audio-decode'
    }
};
const { createProxyMiddleware } = require("http-proxy-middleware");

module.exports = function (app) {
    const targetHost = 'localhost:8080';
    app.use(
        createProxyMiddleware("/graphql", {
            target: `http://${targetHost}/`, // API のベース URL
            changeOrigin: true,
        })
    );

    app.use(
        createProxyMiddleware("/graphql", {
            target: `ws://${targetHost}/`, // API のベース URL
            changeOrigin: true,
            ws: true
        })
    );
};

const { createProxyMiddleware } = require("http-proxy-middleware");

module.exports = function (app) {
    app.use(
        createProxyMiddleware("/graphql", {
            target: "http://localhost:8080/", // API のベース URL
            changeOrigin: true,
        })
    );

    app.use(
        createProxyMiddleware("/graphql", {
            target: "ws://localhost:8080/", // API のベース URL
            changeOrigin: true,
            ws: true
        })
    );
};

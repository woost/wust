angular.module("wust.api").config(HttpAuthConfig);

HttpAuthConfig.$inject = ["$httpProvider", "jwtInterceptorProvider"];

function HttpAuthConfig($httpProvider, jwtInterceptorProvider) {
    jwtInterceptorProvider.authHeader = "X-Auth-Token";
    jwtInterceptorProvider.authPrefix = "";
    jwtInterceptorProvider.tokenGetter = (config, Auth) => {
        // Skip authentication for any requests ending in .html
        if (_.endsWith(config.url, ".html")) {
            return null;
        }

        if (Auth.isLoggedIn)
            return Auth.current.token;
        else
            return undefined;
    };

    $httpProvider.interceptors.push("jwtInterceptor");
}

angular.module("wust.api").config(function($httpProvider, $authProvider) {
    $httpProvider.interceptors.push(function($q, $injector) {
        return {
            request: function(request) {
                var $auth = $injector.get("$auth");
                if ($auth.isAuthenticated()) {
                    request.headers["X-Auth-Token"] = $auth.getToken();
                }

                return request;
            },

            responseError: function(rejection) {
                if (rejection.status === 401) {
                    humane.error("Please login");
                }
                return $q.reject(rejection);
            }
        };
    });

    // Auth config
    $authProvider.httpInterceptor = true; // Add Authorization header to HTTP request
    $authProvider.loginOnSignup = true;
    $authProvider.loginRedirect = "/";
    $authProvider.logoutRedirect = "/";
    $authProvider.signupRedirect = "/";
    $authProvider.loginUrl = "/api/v1/auth/signin/credentials";
    $authProvider.signupUrl = "/api/v1/auth/signup";
    $authProvider.loginRoute = "/";
    $authProvider.signupRoute = "/";
    $authProvider.tokenName = "token";
    $authProvider.tokenPrefix = "satellizer"; // Local Storage name prefix
    $authProvider.authHeader = "X-Auth-Token";
    $authProvider.platform = "browser"; // or "mobile"
    $authProvider.storage = "localStorage"; // or "sessionStorage"
    // $authProvider.unlinkUrl = "/auth/unlink/";
    // $authProvider.unlinkMethod = "get";
    // $authProvider.tokenRoot = false; // set the token parent element if the token is not the JSON root
    // $authProvider.withCredentials = true;


    $authProvider.github({
        clientId: "0ba2600b1dbdb756688b",
        url: "/api/v1/auth/signin/github",
        redirectUri: window.location.origin || window.location.protocol + "//" + window.location.host + "/",
        display: "popup",
        type: "2.0",
        popupOptions: { width: 481, height: 269 }
    });
});

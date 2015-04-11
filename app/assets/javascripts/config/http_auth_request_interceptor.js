angular.module("wust").factory("httpAuthRequestInterceptor", function($injector) {
    return {
        request: function(request) {
            let auth = $injector.get("Auth");
            return auth.authenticateRequest(request);
        }
    };
}).config(function($httpProvider) {
    $httpProvider.interceptors.push("httpAuthRequestInterceptor");
});

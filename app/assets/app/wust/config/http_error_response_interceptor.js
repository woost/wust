angular.module("wust").factory("httpErrorResponseInterceptor", httpErrorResponseInterceptor).config(HttpErrorConfig);

httpErrorResponseInterceptor.$inject = ["$q"];

function httpErrorResponseInterceptor($q) {
    return {
        response: function(responseData) {
            return responseData;
        },
        responseError: function error(response) {
            humane.error(`Server says:<br/>${response.status} - ${JSON.stringify(response.data)}`);
            return $q.reject(response);
        }
    };
}

HttpErrorConfig.$inject = ["$httpProvider"];

function HttpErrorConfig($httpProvider) {
    $httpProvider.interceptors.push("httpErrorResponseInterceptor");
}

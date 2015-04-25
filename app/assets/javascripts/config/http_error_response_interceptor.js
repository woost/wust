angular.module("wust").factory("httpErrorResponseInterceptor", function($q) {
    return {
        response: function(responseData) {
            return responseData;
        },
        responseError: function error(response) {
            humane.error(`Server says:<br/>${response.status} - ${JSON.stringify(response.data)}`);
            return $q.reject(response);
        }
    };
}).config(function($httpProvider) {
    $httpProvider.interceptors.push("httpErrorResponseInterceptor");
});

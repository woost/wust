angular.module("wust.api").factory("httpErrorResponseInterceptor", httpErrorResponseInterceptor).config(HttpErrorConfig);

httpErrorResponseInterceptor.$inject = ["$q", "$injector"];

function httpErrorResponseInterceptor($q, $injector) {
    return {
        response: function(responseData) {
            return responseData;
        },
        responseError: function error(response) {
            console.log(response);
            humane.error(`Server says:<br/>${response.status} - ${JSON.stringify(response.data)}`);
            if (response.status === 404)
                $injector.get("$state").go("dashboard");

            return $q.reject(response);
        }
    };
}

HttpErrorConfig.$inject = ["$httpProvider"];

function HttpErrorConfig($httpProvider) {
    $httpProvider.interceptors.push("httpErrorResponseInterceptor");
}

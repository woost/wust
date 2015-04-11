angular.module("wust").factory("httpErrorResponseInterceptor", function($q /*, $injector*/ ) {
    return {
        response: function(responseData) {
            return responseData;
        },
        responseError: function error(response) {
            // we need to use the injector to get a reference to the $state service in an
            // http-interceptor, otherwise we would have circular dependency.
            // http://stackoverflow.com/questions/20230691/injecting-state-ui-router-into-http-interceptor-causes-circular-dependency
            //var state = $injector.get("$state");
            //state.go("/");
            humane.error(`Server says:<br/>${response.status} - ${JSON.stringify(response.data)}`);

            return $q.reject(response);
        }
    };
}).config(function($httpProvider) {
    $httpProvider.interceptors.push("httpErrorResponseInterceptor");
});

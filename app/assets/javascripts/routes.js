app.config(function($stateProvider, $urlRouterProvider, $locationProvider) {
    $stateProvider.state('help', {
        url: '/help',
        templateUrl: 'views/help.html',
    }).state('graphs', {
        url: '/graphs',
        templateUrl: 'views/graph_list.html',
        controller: 'GraphListsCtrl',
        resolve: {
            initialData: function(Graph) {
                return Graph.get().$promise;
            }
        }
    }).state('graphs.detail', {
        parent: 'graphs',
        url: '/:id',
        templateUrl: 'views/graph_detail.html',
        controller: 'GraphDetailsCtrl',
        resolve: {
            initialData: function(Graph, $stateParams) {
                return Graph.get($stateParams.id).$promise;
            }
        }
    });

    $urlRouterProvider.when('/', '/graphs');
    $urlRouterProvider.otherwise('/');

    $locationProvider.html5Mode(true);
});

app.factory('httpErrorResponseInterceptor', function($q, $injector) {
    return {
        response: function(responseData) {
            return responseData;
        },
        responseError: function error(response) {
            // we need to use the injector to get a reference to the $state service in an
            // http-interceptor, otherwise we would have circular dependency.
            // http://stackoverflow.com/questions/20230691/injecting-state-ui-router-into-http-interceptor-causes-circular-dependency
            //var state = $injector.get('$state');
            //state.go('/');
            // toastr.error("Request failed");

            return $q.reject(response);
        }
    };
});

app.config(function($httpProvider) {
    $httpProvider.interceptors.push('httpErrorResponseInterceptor');
});

app.config(function($stateProvider, $urlRouterProvider, $locationProvider) {
    $stateProvider.state('home', {
        url: '/',
        templateUrl: 'assets/views/home.html',
        controller: 'HomeCtrl',
    }).state('help', {
        url: '/help',
        templateUrl: 'assets/views/help.html',
    }).state('graphs', {
        url: '/graphs',
        templateUrl: 'assets/views/graph.html',
        controller: 'GraphsCtrl',
        resolve: {
            initialData: function(Graph) {
                return Graph.get().$promise;
            }
        }
    }).state('problems', {
        url: '/problems/:id',
        templateUrl: 'assets/views/problem.html',
        controller: 'ProblemsCtrl',
        abstract: true
    }).state('problems.view', {
        parent: "problems",
        url: '/view',
        templateUrl: 'assets/views/problem_view.html',
        controller: 'ProblemViewsCtrl',
    }).state('problems.idea', {
        parent: "problems",
        url: '/idea/:ideaId',
        templateUrl: 'assets/views/problem_idea.html',
        controller: 'ProblemIdeasCtrl',
    });

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
            toastr.error("Request failed");

            return $q.reject(response);
        }
    };
});

app.config(function($httpProvider) {
    $httpProvider.interceptors.push('httpErrorResponseInterceptor');
});

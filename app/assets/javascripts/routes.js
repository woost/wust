app.config(function($stateProvider, $urlRouterProvider, $locationProvider) {
    $stateProvider.state('home', {
        url: '/',
        templateUrl: 'assets/views/home.html',
        controller: 'HomeCtrl',
    }).state('help', {
        url: '/help',
        templateUrl: 'assets/views/help.html',
    }).state('vote', {
        url: '/vote',
        templateUrl: 'assets/views/vote.html',
        controller: 'VotesCtrl',
    }).state('graphs', {
        url: '/graphs',
        templateUrl: 'assets/views/graph.html',
        controller: 'GraphsCtrl',
    }).state('goals', {
        url: '/focus/goals/:id',
        templateUrl: 'assets/views/focus_view.html',
        controller: 'GoalsCtrl',
    }).state('problems', {
        url: '/focus/problems/:id',
        templateUrl: 'assets/views/focus_view.html',
        controller: 'ProblemsCtrl',
    }).state('ideas', {
        url: '/focus/ideas/:id',
        templateUrl: 'assets/views/focus_view.html',
        controller: 'IdeasCtrl',
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

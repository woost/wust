app.config(function($stateProvider, $urlRouterProvider, $locationProvider) {
    $stateProvider.state('home', {
        url: '/',
        templateUrl: 'home.html',
        controller: 'HomeCtrl'
    }).state('graph', {
        url: '/graphs/:id',
        templateUrl: 'graphs/show.html',
        controller: 'GraphsCtrl',
        resolve: {
            initialData: function(GraphResolver, $stateParams) {
                return GraphResolver($stateParams.id);
            }
        }
    });

    $urlRouterProvider.otherwise('/');

    $locationProvider.html5Mode(true);
});

app.config(function($stateProvider, $urlRouterProvider, $locationProvider) {
    $stateProvider.state('help', {
        url: '/help',
        templateUrl: 'help.html',
    }).state('graph', {
        url: '/graphs/:id',
        templateUrl: 'graph_detail.html',
        controller: 'GraphDetailsCtrl',
        resolve: {
            initialData: function(Graph, $stateParams) {
                return Graph.get($stateParams.id).$promise;
            }
        }
    }).state('graphs', {
        url: '/graphs',
        templateUrl: 'graph_list.html',
        controller: 'GraphListsCtrl',
        resolve: {
            initialData: function(Graph, $stateParams) {
                return Graph.get().$promise;
            }
        }
    });

    $urlRouterProvider.when('/', '/graphs');
    $urlRouterProvider.otherwise('/');

    $locationProvider.html5Mode(true);
});

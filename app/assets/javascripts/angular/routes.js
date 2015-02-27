app.config(function($stateProvider, $urlRouterProvider, $locationProvider) {
    $stateProvider.state('help', {
        url: '/help',
        templateUrl: 'help.html',
    }).state('graphs', {
        url: '/graphs',
        templateUrl: 'graph_list.html',
        controller: 'GraphListsCtrl',
        resolve: {
            initialData: function(Graph, $stateParams) {
                return Graph.get().$promise;
            }
        }
    }).state('graphs.detail', {
        parent: 'graphs',
        url: '/:id',
        templateUrl: 'graph_detail.html',
        controller: 'GraphDetailsCtrl',
    });

    $urlRouterProvider.when('/', '/graphs');
    $urlRouterProvider.otherwise('/');

    $locationProvider.html5Mode(true);
});

angular.module("wust").config(function($stateProvider, $urlRouterProvider, $locationProvider, SchemaInfo) {
    $stateProvider.state("page", {
        url: "/",
        abstract: true,
        views: {
            "navigation": {
                templateUrl: "assets/views/navigation.html",
                controller: "NavigationCtrl",
            },
            "content": {
                template: "<div ui-view></div>",
            },
            "footer": {
                templateUrl: "assets/views/footer.html",
                controller: "FooterCtrl",
            }
        }
    }).state("browse", {
        parent: "page",
        url: "browse",
        templateUrl: "assets/views/browse.html",
        controller: "BrowseCtrl",
    }).state("vote", {
        parent: "page",
        url: "vote",
        templateUrl: "assets/views/vote.html",
        controller: "VotesCtrl",
    }).state("graph", {
        parent: "page",
        url: "graph",
        templateUrl: "assets/views/graph.html",
        controller: "GraphsCtrl",
    }).state(SchemaInfo.Goal.state, {
        parent: "page",
        url: "goals/:id",
        templateUrl: "assets/views/focus_view.html",
        controller: "GoalsCtrl",
    }).state(SchemaInfo.Problem.state, {
        parent: "page",
        url: "problems/:id",
        templateUrl: "assets/views/focus_view.html",
        controller: "ProblemsCtrl",
    }).state(SchemaInfo.Idea.state, {
        parent: "page",
        url: "ideas/:id",
        templateUrl: "assets/views/focus_view.html",
        controller: "IdeasCtrl",
    });

    $urlRouterProvider.otherwise("/browse");

    $locationProvider.html5Mode(true);
});

angular.module("wust").config(function($stateProvider, $urlRouterProvider, $locationProvider, DiscourseNodeProvider) {
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
    }).state(DiscourseNodeProvider.setState("Goal", "goals"), {
        parent: "page",
        url: "goals/:id",
        templateUrl: "assets/views/node_focus.html",
        controller: "GoalsCtrl",
    }).state(DiscourseNodeProvider.setState("Problem", "problems"), {
        parent: "page",
        url: "problems/:id",
        templateUrl: "assets/views/node_focus.html",
        controller: "ProblemsCtrl",
    }).state(DiscourseNodeProvider.setState("Idea", "ideas"), {
        parent: "page",
        url: "ideas/:id",
        templateUrl: "assets/views/node_focus.html",
        controller: "IdeasCtrl",
    });

    $urlRouterProvider.otherwise("/browse");

    $locationProvider.html5Mode(true);
});

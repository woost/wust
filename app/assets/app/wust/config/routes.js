angular.module("wust").config(function($stateProvider, $urlRouterProvider, $locationProvider, DiscourseNodeProvider) {
    const templateBase = "assets/app/components";
    $stateProvider.state("page", {
        url: "/",
        abstract: true,
        views: {
            "navigation": {
                templateUrl: `${templateBase}/navigation/navigation.html`,
                controller: "NavigationCtrl",
            },
            "content": {
                template: "<div ui-view></div>",
            },
            "footer": {
                templateUrl: `${templateBase}/footer/footer.html`,
                controller: "FooterCtrl",
            }
        }
    }).state("browse", {
        parent: "page",
        url: "browse",
        templateUrl: `${templateBase}/browse/browse.html`,
        controller: "BrowseCtrl",
    }).state("vote", {
        parent: "page",
        url: "vote",
        templateUrl: `${templateBase}/votes/vote.html`,
        controller: "VotesCtrl",
    }).state("graph", {
        parent: "page",
        url: "graph",
        templateUrl: `${templateBase}/graphs/graph.html`,
        controller: "GraphsCtrl",
    }).state(DiscourseNodeProvider.setState("Goal", "goals"), {
        parent: "page",
        url: "goals/:id",
        templateUrl: `${templateBase}/nodes/focus_view.html`,
        controller: "GoalsCtrl",
    }).state(DiscourseNodeProvider.setState("Problem", "problems"), {
        parent: "page",
        url: "problems/:id",
        templateUrl: `${templateBase}/nodes/focus_view.html`,
        controller: "ProblemsCtrl",
    }).state(DiscourseNodeProvider.setState("Idea", "ideas"), {
        parent: "page",
        url: "ideas/:id",
        templateUrl: `${templateBase}/nodes/focus_view.html`,
        controller: "IdeasCtrl",
    });

    $urlRouterProvider.otherwise("/browse");

    $locationProvider.html5Mode(true);
});

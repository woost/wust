angular.module("wust").config(RoutesConfig);

RoutesConfig.$inject = ["$stateProvider", "$urlRouterProvider", "$locationProvider", "DiscourseNodeProvider"];

function RoutesConfig($stateProvider, $urlRouterProvider, $locationProvider, DiscourseNodeProvider) {
    const templateBase = "assets/app/components";
    $stateProvider.state("page", {
        url: "/",
        abstract: true,
        views: {
            "navigation": {
                templateUrl: `${templateBase}/navigation/navigation.html`,
                controller: "NavigationCtrl as vm",
            },
            "content": {
                template: "<div ui-view></div>",
            },
            "footer": {
                templateUrl: `${templateBase}/footer/footer.html`,
                controller: "FooterCtrl as vm",
            }
        }
    }).state("browse", {
        parent: "page",
        url: "browse",
        templateUrl: `${templateBase}/browse/browse.html`,
        controller: "BrowseCtrl as vm",
    }).state("vote", {
        parent: "page",
        url: "vote",
        templateUrl: `${templateBase}/votes/vote.html`,
        controller: "VotesCtrl as vm",
    }).state("graph", {
        parent: "page",
        url: "graph",
        templateUrl: `${templateBase}/graphs/graph.html`,
        controller: "GraphsCtrl as vm",
    }).state("users", {
        parent: "page",
        url: "users",
        templateUrl: `${templateBase}/users/list.html`,
        controller: "UserListsCtrl as vm",
    }).state(DiscourseNodeProvider.setState("User", "user"), {
        parent: "page",
        url: "users/:id",
        templateUrl: `${templateBase}/users/detail.html`,
        controller: "UserDetailsCtrl as vm",
    }).state(DiscourseNodeProvider.setState("Goal", "goals"), {
        parent: "page",
        url: "goals/:id",
        templateUrl: `${templateBase}/nodes/focus_view.html`,
        controller: "GoalsCtrl as vm",
    }).state(DiscourseNodeProvider.setState("Problem", "problems"), {
        parent: "page",
        url: "problems/:id",
        templateUrl: `${templateBase}/nodes/focus_view.html`,
        controller: "ProblemsCtrl as vm",
    }).state(DiscourseNodeProvider.setState("Idea", "ideas"), {
        parent: "page",
        url: "ideas/:id",
        templateUrl: `${templateBase}/nodes/focus_view.html`,
        controller: "IdeasCtrl as vm",
    });

    $urlRouterProvider.otherwise("/browse");

    $locationProvider.html5Mode(true);
}

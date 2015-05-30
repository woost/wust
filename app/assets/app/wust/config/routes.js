angular.module("wust").config(RoutesConfig);

RoutesConfig.$inject = ["$stateProvider", "$urlRouterProvider", "$locationProvider", "DiscourseNodeProvider"];

function RoutesConfig($stateProvider, $urlRouterProvider, $locationProvider, DiscourseNodeProvider) {
    const templateBase = "assets/app/components";
    $stateProvider.state("page", {
        abstract: true,
        views: {
            "navigation": {
                templateUrl: `${templateBase}/navigation/navigation.html`,
                controller: "NavigationCtrl as vm",
            },
            "content": {
                templateUrl: `${templateBase}/content/content.html`,
                controller: "ContentCtrl as vm",
            },
            "footer": {
                templateUrl: `${templateBase}/footer/footer.html`,
                controller: "FooterCtrl as vm",
            }
        }
    }).state("dashboard", {
        parent: "page",
        url: "/dashboard",
        templateUrl: `${templateBase}/dashboard/dashboard.html`,
        controller: "DashboardCtrl as vm",
    }).state("vote", {
        parent: "page",
        url: "/vote",
        templateUrl: `${templateBase}/votes/vote.html`,
        controller: "VotesCtrl as vm",
    }).state("graph", {
        parent: "page",
        url: "/graph",
        templateUrl: `${templateBase}/graphs/graph.html`,
        controller: "GraphsCtrl as vm",
    }).state("branches", {
        parent: "page",
        url: "/branches/:id",
        templateUrl: `${templateBase}/branches/branch.html`,
        controller: "BranchesCtrl as vm"
    }).state("columns", {
        parent: "page",
        url: "/columns/:id",
        templateUrl: `${templateBase}/columns/column.html`,
        controller: "ColumnsCtrl as vm"
    }).state("users", {
        abstract: true,
        parent: "page",
        url: "/users",
        templateUrl: `${templateBase}/users/user.html`,
    }).state("users.list", {
        url: "/list",
        templateUrl: `${templateBase}/users/list.html`,
        controller: "UserListsCtrl as vm",
    }).state(DiscourseNodeProvider.setState("User", "users.details"), {
        url: "/details/:id",
        templateUrl: `${templateBase}/users/detail.html`,
        controller: "UserDetailsCtrl as vm",
    }).state(DiscourseNodeProvider.setState("Post", "posts"), {
        parent: "page",
        url: "/posts/:id",
        templateUrl: `${templateBase}/nodes/focus_view.html`,
        controller: "PostsCtrl as vm",
    });

    $urlRouterProvider.otherwise("/dashboard");

    $locationProvider.html5Mode(true);
}

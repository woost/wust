angular.module("wust.config").config(RoutesConfig);

RoutesConfig.$inject = ["$stateProvider", "$urlRouterProvider", "$locationProvider", "DiscourseNodeProvider"];

function RoutesConfig($stateProvider, $urlRouterProvider, $locationProvider, DiscourseNodeProvider) {
    const templateBase = "assets/app/components";
    $stateProvider.state("page", {
        abstract: true,
        templateUrl: `${templateBase}/page/page.html`,
        controller: "PageCtrl as vm",
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
    }).state("tags", {
        abstract: true,
        parent: "page",
        url: "/tags",
        templateUrl: `${templateBase}/tags/tag.html`,
    }).state("tags.list", {
        url: "/list",
        templateUrl: `${templateBase}/tags/list.html`,
        controller: "TagListsCtrl as vm",
    }).state(DiscourseNodeProvider.setState("TagLike", "tags.details"), {
        url: "/details/:id",
        templateUrl: `${templateBase}/tags/detail.html`,
        controller: "TagDetailsCtrl as vm",
    }).state(DiscourseNodeProvider.setState("Post", "focus"), {
        parent: "page",
        url: "/focus/:id",
        templateUrl: `${templateBase}/focus/focus.html`,
        controller: "FocusCtrl as vm",
        resolve: {
            component: ["ConnectedComponents","$stateParams", function(ConnectedComponents, $stateParams) {
                return ConnectedComponents.$find($stateParams.id).$rawGraphPromise();
            }]
        }
    });

    $urlRouterProvider.otherwise("/dashboard");

    $locationProvider.html5Mode(true);

    //TODO: https://github.com/angular/angular.js/issues/8934https://github.com/angular/angular.js/issues/8934
    // should fix our problem with paths to marker defs
    // $locationProvider.html5Mode({enabled: true, requireBase: false});
}

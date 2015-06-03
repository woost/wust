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
            "scratchpad": {
                templateUrl: `${templateBase}/scratchpad/scratchpad.html`,
                controller: "ScratchpadCtrl as vm",
            },
            "search": {
                templateUrl: `${templateBase}/search/search.html`,
                controller: "SearchCtrl as vm",
            },
            "content": {
                template: "<div ui-view></div>",
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
    }).state(DiscourseNodeProvider.setState("Post", "focus"), {
        parent: "page",
        url: "/focus/:id",
        templateUrl: `${templateBase}/focus/focus.html`,
        controller: "FocusCtrl as vm",
        resolve: {
            component: ["ConnectedComponents","$stateParams", function(ConnectedComponents, $stateParams) {
                return ConnectedComponents.$find($stateParams.id).$asPromise();
            }]
        }
    });

    $urlRouterProvider.otherwise("/dashboard");

    $locationProvider.html5Mode(true);
}

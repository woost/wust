angular.module("wust.config").config(RoutesConfig);

RoutesConfig.$inject = ["$stateProvider", "$urlRouterProvider", "$locationProvider"];

function RoutesConfig($stateProvider, $urlRouterProvider, $locationProvider) {
    $stateProvider.state("page", {
        abstract: true,
        templateUrl: `components/page/page.html`,
        controller: "PageCtrl as vm",
    }).state("dashboard", {
        parent: "page",
        url: "/dashboard",
        templateUrl: `components/dashboard/dashboard.html`,
        controller: "DashboardCtrl as vm",
    }).state("vote", {
        parent: "page",
        url: "/vote",
        templateUrl: `components/votes/vote.html`,
        controller: "VotesCtrl as vm",
    }).state("users", {
        abstract: true,
        parent: "page",
        url: "/users",
        templateUrl: `components/users/user.html`,
    }).state("users.list", {
        url: "",
        templateUrl: `components/users/list.html`,
        controller: "UserListsCtrl as vm",
    }).state("users.details", {
        url: "/:id",
        templateUrl: `components/users/detail.html`,
        controller: "UserDetailsCtrl as vm",
    }).state("tags", {
        parent: "page",
        url: "/tags",
        templateUrl: `components/tags/tag.html`,
        controller: "TagsCtrl as vm",
    }).state("tags.details", {
        url: "/:id",
        templateUrl: `components/tags/tag_detail.html`,
        controller: "TagDetailsCtrl as vm",
    }).state("focus", {
        parent: "page",
        url: "/focus/:id/:type",
        templateUrl: `components/focus/focus.html`,
        controller: "FocusCtrl as vm",
        resolve: { //TODO: should not use resolve, instead async resolve in focus ctrl
            rootNode: ["Post","$stateParams","$state", function(Post, $stateParams, $state) {
                // somehow the resolve method is called every time we change
                // our url for switching between graph and neighbours view.
                // so if we are still viewing the same node, we just return
                // undefined, as no new focusctrl is instantiated
                if ($state.is("focus", {
                    id: $stateParams.id
                }) || $state.is("focus", {
                    id: $stateParams.id,
                    type: "graph"
                })) {
                    return undefined;
                } else {
                    return Post.$find($stateParams.id).$asPromise();
                }
            }]
        }
    });

    $urlRouterProvider.otherwise("/dashboard");

    $locationProvider.html5Mode(true);

    //TODO: https://github.com/angular/angular.js/issues/8934https://github.com/angular/angular.js/issues/8934
    // should fix our problem with paths to marker defs
    // $locationProvider.html5Mode({enabled: true, requireBase: false});
}

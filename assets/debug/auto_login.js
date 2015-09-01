angular.module("wust.api").run(function(Auth) {
    if (!Auth.checkLoggedIn()) {
        Auth.register({
            identifier: Math.random().toString(36).substring(7),
            password: "hans"
        });
    }
});

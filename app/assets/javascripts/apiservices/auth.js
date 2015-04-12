angular.module("wust").service("Auth", function(restmod, jwtHelper, store, authService) {
    let authStore = store.getNamespacedStore("auth");
    let service = {
        signup: restmod.model("/auth/signup"),
        signin: restmod.model("/auth/signin"),
        signout: restmod.singleton("/auth/signout")
    };

    this.login = _.partial(authenticate, service.signin, "Logged in");
    this.register = _.partial(authenticate, service.signup, "Registered");
    this.logout = logout;
    this.loggedIn = loggedIn;
    this.getUsername = _.wrap("identifier", getProperty);
    this.getToken = _.wrap("token", getProperty);

    function loggedIn() {
        let currentUser = getCurrentUser();
        return currentUser && !jwtHelper.isTokenExpired(currentUser.token);
    }

    function getProperty(name) {
        return loggedIn() ? getCurrentUser()[name] : undefined;
    }

    function authenticate(model, message, user) {
        model.$create(user).$then(response => {
            authStore.set("currentUser", _.pick(response, "identifier", "token"));
            authService.loginConfirmed("success");
            humane.success(message);
        });
    }

    function logout() {
        // TODO: should this really be a get request
        service.signout.$fetch().$then(response => {
            authStore.remove("currentUser");
            authService.loginCancelled();
            humane.success("Logged out");
        });
    }

    function getCurrentUser() {
        return authStore.get("currentUser");
    }
});

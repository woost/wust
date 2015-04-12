angular.module("wust").service("Auth", function(restmod, jwtHelper, authService) {
    let service = {
        signup: restmod.model("/auth/signup"),
        signin: restmod.model("/auth/signin"),
        signout: restmod.singleton("/auth/signout")
    };

    let currentUser;

    this.login = _.partial(authenticate, service.signin, "Logged in");
    this.register = _.partial(authenticate, service.signup, "Registered");
    this.logout = logout;
    this.loggedIn = loggedIn;
    this.getUsername = _.wrap("identifier", getProperty);
    this.getToken = _.wrap("token", getProperty);

    function loggedIn() {
        return (currentUser !== undefined) && !jwtHelper.isTokenExpired(currentUser.token);
    }

    function getProperty(name) {
        return loggedIn() ? currentUser[name] : undefined;
    }

    function authenticate(model, message, user) {
        model.$create(user).$then(response => {
            currentUser = _.pick(response, "identifier", "token");
            authService.loginConfirmed("success");
            humane.success(message);
        });
    }

    function logout() {
        // TODO: should this really be a get request
        service.signout.$fetch().$then(response => {
            currentUser = undefined;
            authService.loginCancelled();
            humane.success("Logged out");
        });
    }
});

angular.module("wust").service("Auth", function(restmod, authService) {
    let service = {
        signup: restmod.model("/auth/signup"),
        signin: restmod.model("/auth/signin"),
        signout: restmod.singleton("/auth/signout")
    };

    let currentUser;

    this.login = login;
    this.register = register;
    this.logout = logout;
    this.loggedIn = loggedIn;
    this.getUsername = getUsername;
    this.authenticateRequest = authenticateRequest;

    function loggedIn() {
        return currentUser !== undefined;
    }

    function getUsername() {
        return (currentUser || {}).identifier;
    }

    function authenticateRequest(config) {
        if (!currentUser) {
            return config;
        }

        config.headers["X-Auth-Token"] = currentUser.token;
        return config;
    }

    function handleAuthentication(message, response) {
        currentUser = response;
        authService.loginConfirmed("success", authenticateRequest);
        humane.success(message);
    }

    function login(user) {
        service.signin.$create(user).$then(_.wrap("Logged in", handleAuthentication));
    }

    function register(user) {
        service.signup.$create(user).$then(_.wrap("Registered", handleAuthentication));
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

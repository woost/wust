angular.module("wust").service("Auth", function(restmod) {
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

    function loggedIn() {
        return currentUser !== undefined;
    }

    function login(user) {
        return service.signin.$create(user).$then(response => {
            currentUser = response;
            humane.success("Logged in");
        });
    }

    function register(user) {
        return service.signup.$create(user).$then(response => {
            currentUser = response;
            humane.success("Registered");
        });
    }

    function logout() {
        // TODO: should this really be a get request
        return service.signout.$fetch().$then(response => {
            currentUser = undefined;
            humane.success("Logged out");
        });
    }
});

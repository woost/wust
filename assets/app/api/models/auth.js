angular.module("wust.api").service("Auth", Auth);

Auth.$inject = ["$rootScope", "$window", "restmod", "jwtHelper", "store"];

function Auth($rootScope, $window, restmod, jwtHelper, store) {
    let self = this;

    let authStore = store.getNamespacedStore("auth");
    let userKey = "currentUser";
    let messageKey = "welcomeMessage";

    let service = {
        signup: restmod.model("/auth/signup"),
        signin: restmod.model("/auth/signin"),
        signout: restmod.singleton("/auth/signout")
    };

    this.login = _.partial(authenticate, service.signin, undefined);
    this.register = _.partial(authenticate, service.signup, "Successfully registered account");
    this.logout = logout;
    this.current = authStore.get(userKey) || {};

    let welcomeMessage = authStore.get(messageKey);
    if (welcomeMessage) {
        authStore.set(messageKey, undefined);
        humane.success(welcomeMessage);
    }

    if (checkLoggedIn()) {
        this.isLoggedIn = true;
    } else {
        this.isLoggedIn = false;
    }

    // every time the window gets focused, clear the inMemoryCache of the store,
    // so all changes in store get propagated into our current angular session.
    // if the value has changed, we trigger rerendering of the whole.
    // this is only needed if storage is available, otherwise we won't share data between buffers.
    if (authStore.storageAvailable) {
        $window.onfocus = () => {
            let prev = authStore.get(userKey) || {};
            delete authStore.inMemoryCache[userKey];
            let response = authStore.get(userKey) || {};
            self.current.identifier = response.identifier;
            self.current.token = response.token;
            self.current.userId = response.userId;

            checkLoggedIn();
            if (prev.userId !== self.current.userId)
                location.reload();
        };
    }

    function checkLoggedIn() {
        if (self.current.token) {
            if (jwtHelper.isTokenExpired(self.current.token)) {
                logoutLocally();
                return false;
            } else {
                return true;
            }
        }

        return false;
    }

    function authenticate(model, message, user) {
        model.$create(user).$then(response => {
            authStore.set(userKey, _.pick(response, "identifier", "userId", "token"));
            if (message)
                authStore.set(messageKey, message);

            location.reload();
        }, resp => humane.error(resp.$response.data));
    }

    function logout(withReload = true) {
        // TODO: should this really be a get request
        // also it does not do anything, except telling the server "i clicked logout" - who cares? we just forget the jwt token locally.
        service.signout.$fetch().$then(response => {
            logoutLocally(withReload);
        }, () => logoutLocally(withReload));
    }

    function logoutLocally(withReload = true) {
        if(!self.current.identifier) return;

        authStore.remove(userKey);
        if(withReload)
            location.reload();
    }
}

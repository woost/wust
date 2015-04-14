angular.module("wust.api").service("Auth", function($rootScope, $window, restmod, jwtHelper, store) {
    // use localstorage (fallback to cookies) for persisting the current user
    // and the jwt token
    let authStore = store.getNamespacedStore("auth");
    let userKey = "currentUser";

    // setup restmod models for authentication
    let service = {
        signup: restmod.model("/auth/signup"),
        signin: restmod.model("/auth/signin/credentials"),
        oauth: {
            github: restmod.model("/auth/signin/github")
        },
        link: {
            github: restmod.model("/auth/link/github")
        },
        signout: restmod.singleton("/auth/signout")
    };

    // public methods
    this.register = _.partial(authenticate, service.signup, "Registered");
    this.login = _.partial(authenticate, service.signin, "Logged in");
    this.oauth = _.mapValues(service.oauth, model => _.wrap(model, oauth));
    this.link = _.mapValues(service.link, model => _.wrap(model, link));
    this.logout = logout;
    this.loggedIn = loggedIn;
    this.getUsername = _.wrap("identifier", getProperty);
    this.getToken = _.wrap("token", getProperty);

    // every time the window gets focused, clear the inMemoryCache of the store,
    // so all changes in store get propagated into our current angular session.
    // if the value has changed, we trigger rerendering of the whole page.
    $window.onfocus = () => {
        let prev = authStore.get(userKey);
        delete authStore.inMemoryCache[userKey];
        if (prev !== authStore.get(userKey)) {
            $rootScope.$apply();
        }
    };

    function loggedIn() {
        let currentUser = authStore.get(userKey);
        return currentUser && !jwtHelper.isTokenExpired(currentUser.token);
    }

    function getProperty(name) {
        return loggedIn() ? authStore.get(userKey)[name] : undefined;
    }

    function link(model, user) {
        model.$create(user).$then(response => {
            humane.success("Linked");
        });
    }

    function oauth(model) {
        model.$create().$then(response => {
            console.log(response);
        });
    }

    function authenticate(model, message, user) {
        model.$create(user).$then(response => {
            authStore.set(userKey, _.pick(response, "identifier", "token"));
            humane.success(message);
        });
    }

    function logout() {
        // TODO: should this really be a get request
        console.log(service.signout.$fetch().$then(response => {
            logoutLocally();
            humane.success("Logged out");
        }).$promise.catch(logoutLocally));
    }

    function logoutLocally() {
        authStore.remove(userKey);
    }
});

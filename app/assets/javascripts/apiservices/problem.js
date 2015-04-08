angular.module("wust").factory("Problem", function(restmod) {
    return restmod.model("/problems").mix({
        goals: { hasMany: "Goal" },
        // FIXME: cannot reference self...
        problems: { hasMany: restmod.model("/problems") },
        ideas: { hasMany: "Idea" },
    });
});

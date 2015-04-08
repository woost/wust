angular.module("wust").factory("Idea", function(restmod) {
    return restmod.model("/ideas").mix({
        goals: { hasMany: "Goal" },
        problems: { hasMany: "Problem" },
        ideas: { hasMany: restmod.model("/ideas") },
    });
});

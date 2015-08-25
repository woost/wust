#!/usr/bin/env node

var fs = require("fs");
var sane = require("sane");
var broccoli = require("broccoli");
var Watcher = require("broccoli-sane-watcher");
var ncp = require("ncp");
var path = require("path");

var destDir = "public"

var appRoot = process.env.APP_ROOT || process.cwd();
var brocfile = process.env.BROCFILE || path.join(__dirname, "Brocfile.js");

console.log("Reading Brocfile", require.resolve(brocfile), "\n");

process.chdir(appRoot);

try {
    fs.mkdirSync(destDir);
} catch (e) {}

var tree = require(brocfile);
var builder = new broccoli.Builder(tree);

var broccoliWatcher = new Watcher(builder, {verbose: true});
broccoliWatcher.on("change", function(hash) {
  console.log("Build finished in " + Math.round(hash.totalTime / 1e6) + " ms");
  ncp(hash.directory, path.join(appRoot, destDir));
  return hash;
});

broccoliWatcher.on("error", function(error) {
  console.log("ERROR", error.message);
});

process.addListener("exit", function () {
  console.log("exiting");
  builder.cleanup();
});

process.on("SIGINT", function () {
  process.exit(1);
});

process.on("SIGTERM", function () {
  process.exit(1);
});

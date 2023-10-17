// Used only if jahia-ui-root is the host, experimental
import('@jahia/app-shell/bootstrap').then(res => {
    console.log(res);
    window.jahia = res;
    res.startAppShell(window.appShell.remotes, window.appShell.targetId);
});

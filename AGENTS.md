# cringebook - notes for agents

> **Hosting (since 2026-10-29, god v2.29.0): the Oracle VM, not Windows.** The
> live copy runs on `oracle.gogillu.live` as the systemd unit `app-cringebook` (user
> `apps`, `/srv/apps/cringebook/{src,bin,data}`, loopback `127.0.0.1:9899`; definition
> `god/ops/oracle-vm/apps/cringebook.conf`). Deploy = push to GitHub, then
> `ssh ubuntu@oracle.gogillu.live sudo appctl deploy cringebook` (build, restart, health
> check, automatic rollback); logs: `journalctl -u app-cringebook`. The Windows task(s)
> CringebookService are disabled and `scripts/start-cringebook.ps1` refuses to start a second copy - two
> live copies would fork the data. Until the DNS record of cringebook.gogillu.live points at
> `137.23.55.97`, public traffic still enters through god-frontproxy on Windows and
> reaches the VM over the SSH forward on `127.0.0.1:9899` (task `GodOracleAppsTunnel`).
> The Windows `data/` folder is a stale pre-move copy. It runs the prebuilt jar as `/srv/apps/cringebook/bin/app.jar` (rebuild with `./mvnw -DskipTests package` and copy `target/app-0.0.1-SNAPSHOT.jar` there, or install a JDK on the VM). Database `data` lives in the VM's localhost-only MySQL 8.4 under its own user `cringebook` (credentials in `/etc/apps/cringebook.secret.env`, overriding the committed root password); the Windows MySQL copy is stale.

# git fork后如何保持与源项目的更新

- 首先保证本地仓库的upstream是源项目的URL，若没有则添加：

> git remote add upstream URL

- 然后利用fetch和merge合并upstream的master分支：

> git fetch upstream
> git merge upstream/master

- 此时本地的master分支就更新至upstream的master版本。然后利用push将本地分支覆盖到git远程分支上：

> git push origin master

- 这就完成fork后保持对源项目的更新。
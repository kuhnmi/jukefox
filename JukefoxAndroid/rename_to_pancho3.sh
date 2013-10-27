svn mv https://dcg-svn.ethz.ch/trunk/collaboration/jukefox/branches/alpha/JukefoxAndroid/src/ch/ethz/dcg/pancho2 https://dcg-svn.ethz.ch/trunk/collaboration/jukefox/branches/alpha/JukefoxAndroid/src/ch/ethz/dcg/pancho3 -m="Changed package to pancho3 (moved dir)"
svn up
grep -lr --exclude=rename_to_pancho* -e 'pancho2' * | xargs sed -i 's/pancho2/pancho3/g'
svn ci -m="Changed package to pancho3 (replaced references)"

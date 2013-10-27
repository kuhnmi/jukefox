svn mv https://dcg-svn.ethz.ch/trunk/collaboration/jukefox/branches/alpha/JukefoxAndroid/src/ch/ethz/dcg/pancho3 https://dcg-svn.ethz.ch/trunk/collaboration/jukefox/branches/alpha/JukefoxAndroid/src/ch/ethz/dcg/pancho2 -m="Changed package to pancho2 (moved dir)"
svn up
grep -lr --exclude=rename_to_pancho* -e 'pancho3' * | xargs sed -i 's/pancho3/pancho2/g'
svn ci -m="Changed package to pancho2 (replaced references)"

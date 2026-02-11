#!/bin/bash
# Script to remove all git history and create a clean initial commit
# 
# WARNING: This will permanently remove all commit history!
# Make sure you have a backup before running this script.
#
# Usage: 
#   1. Make sure you're on the branch you want to clean (e.g., main or master)
#   2. Run this script: ./CLEAN_HISTORY.sh
#   3. Force push to remote: git push origin <branch-name> --force

set -e

echo "⚠️  WARNING: This will remove ALL git history!"
echo "Current branch: $(git branch --show-current)"
echo ""
read -p "Are you sure you want to continue? (type 'yes' to proceed): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Aborted."
    exit 1
fi

CURRENT_BRANCH=$(git branch --show-current)

echo ""
echo "Creating clean history..."
echo ""

# Create orphan branch (branch with no history)
git checkout --orphan temp-clean-history

# Stage all files
git add -A

# Create initial commit
git commit -m "Initial commit"

# Delete old branch and rename new one
git branch -D $CURRENT_BRANCH
git branch -M temp-clean-history $CURRENT_BRANCH

echo ""
echo "✅ Clean history created!"
echo ""
echo "Current commit:"
git log --oneline -1
echo ""
echo "Total commits: $(git rev-list --count HEAD)"
echo ""
echo "⚠️  To complete the process, you need to force push:"
echo "   git push origin $CURRENT_BRANCH --force"
echo ""
echo "Note: Make sure you have backup before force pushing!"

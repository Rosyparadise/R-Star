- TODO: 
    1) Delete for empty rectangles (in tree with >=3 levels)
    2) Root split to create tree with >=3 levels
    3) Range Query
    4) K-NN Query
    5) Skyline Query
    6) Proper messages during the build of the tree, since it needs some time.  
       E.g.:  
       ```
       Block 1: maximum number of nodes reached
       Block 1: Split
       Block 2: maximum number of nodes reached
       Block 2: Reinsert
       Block 3: maximum number of nodes reached
       Block 3: Reinsert
       Block 3: Split
       ```
    7) Menu in terminals for user input etc.
- Reproduce delete that leads to less than 40% of maximum entries in a rectangle:
    1) Uncomment the loop in line 426-428 of [FileHandler](https://github.com/Rosyparadise/DatabaseTechnologyProject/blob/main/RStarTree/src/FileHandler.java)
    2) Set minEntries to 670 in line 8 of [Delete](https://github.com/Rosyparadise/DatabaseTechnologyProject/blob/main/RStarTree/src/Delete.java)

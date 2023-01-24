#R* Tree

R* Tree implementation built to work with geographic coordinate data on a byte level. Both point-by-point and bottom-up methods are supported. Once the tree is built, it is saved and can be used at a later time. The user is presented with a simple UI where insertion, deletion and 
querying of points is available. 

Queries:

1) Range
2) K-NN
3) Skyline

Linear versions are provided for both the range and K-NN queries for time comparison.

Data is parsed from an OpenStreetMap file. 

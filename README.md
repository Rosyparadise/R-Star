# R* Tree

R* Tree implementation built to index spatial data based on geographic coordinates. The points are stored in byte form in size-adjustable leaf nodes. Besides a point-by-point tree construction, bottom-up using Hilbert Sort is also available. Once the tree is built, it is saved and can be used at a later time. The user is presented with a UI where insertion, deletion and querying of points is available. 

Queries:

1) Range
2) K-NN
3) Skyline

Linear versions are provided for both the range and K-NN queries for time comparison.

Data is parsed from an OpenStreetMap file. 

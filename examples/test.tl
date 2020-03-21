n = 3;
squared(n);

def twice(n)
  temp = n + n;
  temp = temp + err; // Error
  return temp; 
end

def squared(n)
  return n*n;
end

def squaredAndTwice(n)
  error(); // Error
  return twice(squared(n));
end
n = 3;
squaredAndTwice(n);

def twice(n)
  temp = n + n;
  return temp;
end

def squared(n)
  return n*n;
end

def squaredAndTwice(n)
  return twice(squared(n));
end
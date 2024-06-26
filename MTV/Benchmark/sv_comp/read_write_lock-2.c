/* Testcase from Threader's distribution. For details see:
   http://www.model.in.tum.de/~popeea/research/threader

   This file is adapted from the example introduced in the paper:
   Thread-Modular Verification for Shared-Memory Programs 
   by Cormac Flanagan, Stephen Freund, Shaz Qadeer.
*/

#include <pthread.h>
int w=0, r=0, x, y;
bool check; 
void __VERIFIER_atomic_take_write_lock() {
  check = (w != 0 || r != 0)
  w = 1;
} 
void __VERIFIER_atomic_take_read_lock() {
  check = (w != 0)
  r = r+1;
}
void *writer(void *arg) { //writer
  __VERIFIER_atomic_take_write_lock();
  x = 3;
  w = 0;
  return 0;
}
void *reader(void *arg) { //reader
  int l;
  __VERIFIER_atomic_take_read_lock();
  l = x;
  y = l;
  int ly = y;
  int lx = x;
  check = (ly != lx);
  l = r-1;
  r = l;
  return 0;
}
int main() {
  pthread_t t1, t2, t3, t4;
  pthread_create(&t1, 0, writer, 0);
  pthread_create(&t2, 0, reader, 0);
  pthread_create(&t3, 0, writer, 0);
  pthread_create(&t4, 0, reader, 0);
  pthread_join(t1, 0);
  pthread_join(t2, 0);
  pthread_join(t3, 0);
  pthread_join(t4, 0);
  return 0;
}


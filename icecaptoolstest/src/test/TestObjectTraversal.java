package test;

import reflect.ObjectInfo;
import util.ReferenceIterator;
import vm.HVMHeap;
import vm.Heap;

public class TestObjectTraversal {

    private static class A
    {
        @SuppressWarnings("unused")
        A ref1;
        @SuppressWarnings("unused")
        A ref2;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
       boolean failed = test();
       if (!failed)
       {
           args = null;
       }
    }

    public static boolean test() {
        Heap heap = HVMHeap.getHeap();
        
        A a = new A();
        A b = new A();
        
        a.ref1 = a;
        a.ref2 = b;
        
        int aAddress = ObjectInfo.getAddress(a);
        
        ReferenceIterator references = heap.getRefFromObj(aAddress);
        
        if (references.hasNext())
        {
            int ref1 = references.next();
            if (ref1 == aAddress)
            {
                if (references.hasNext())
                {
                    int ref2 = references.next();
                    if (ref2 == ObjectInfo.getAddress(b))
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}

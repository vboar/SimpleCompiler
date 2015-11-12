#include "stdio.h"
/**
*   这是多行注释
*/
int main() {    // 这是注释
    int a = 0;
    double b = -1.23;
    a = a + 1;
    if (a > 0 && b < 1) {
        a++;
    } else {
        a /= 5;
    }
    while (a > 0) {
        printf("1234567890");
    }
    switch (a) {
    case 1:
        break;
    default:
        a = 1;
    }
    char xyz = 'a';
    char c = '\t';
    c= '\g';    // 错误
    xyz = 'a123';   // 错误
    b = 12.R;   // 错误
    a = ~`; // 错误
}


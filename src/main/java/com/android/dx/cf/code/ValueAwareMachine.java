package com.android.dx.cf.code;

import com.android.dx.rop.cst.CstCallSiteRef;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Prototype;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeBearer;
import com.android.dx.util.Hex;

public class ValueAwareMachine extends BaseMachine {
    public ValueAwareMachine(Prototype prototype) {
        super(prototype);
    }

    public void run(Frame frame, int offset, int opcode) {
        Type type;
        switch (opcode) {
            case 0:
            case 79:
            case 87:
            case 88:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 169:
            case 171:
            case 172:
            case 177:
            case 179:
            case 181:
            case 191:
            case 194:
            case 195:
            case 198:
            case 199:
                clearResult();
                break;
            case 18:
            case 20:
                setResult((TypeBearer) getAuxCst());
                break;
            case 21:
            case 54:
                setResult(arg(0));
                break;
            case 46:
            case 96:
            case 100:
            case 104:
            case 108:
            case 112:
            case 116:
            case 120:
            case ByteOps.ISHR /*122*/:
            case 124:
            case 126:
            case 128:
            case 130:
            case 132:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 190:
                setResult(getAuxType());
                break;
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
                clearResult();
                for (int pattern = getAuxInt(); pattern != 0; pattern >>= 4) {
                    addResult(arg((pattern & 15) - 1));
                }
                break;
            case 168:
                setResult(new ReturnAddress(getAuxTarget()));
                break;
            case 178:
            case 180:
            case 182:
            case 184:
            case 185:
                type = ((TypeBearer) getAuxCst()).getType();
                if (type != Type.VOID) {
                    setResult(type);
                    break;
                } else {
                    clearResult();
                    break;
                }
            case 183:
                Type thisType = arg(0).getType();
                if (thisType.isUninitialized()) {
                    frame.makeInitialized(thisType);
                }
                type = ((TypeBearer) getAuxCst()).getType();
                if (type != Type.VOID) {
                    setResult(type);
                    break;
                } else {
                    clearResult();
                    break;
                }
            case 186:
                type = ((CstCallSiteRef) getAuxCst()).getReturnType();
                if (type != Type.VOID) {
                    setResult(type);
                    break;
                } else {
                    clearResult();
                    break;
                }
            case 187:
                setResult(((CstType) getAuxCst()).getClassType().asUninitialized(offset));
                break;
            case 188:
            case 192:
            case 197:
                setResult(((CstType) getAuxCst()).getClassType());
                break;
            case 189:
                setResult(((CstType) getAuxCst()).getClassType().getArrayType());
                break;
            case 193:
                setResult(Type.INT);
                break;
            default:
                throw new RuntimeException("shouldn't happen: " + Hex.u1(opcode));
        }
        storeResults(frame);
    }
}
